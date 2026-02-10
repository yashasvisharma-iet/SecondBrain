import { useEffect, useRef } from 'react'
import * as d3 from 'd3'

import { type GraphData, type GraphEdge, type GraphNode } from './GraphTypes'

type Props = {
  data: GraphData
  width?: number
  height?: number
  onNodeSelect?: (nodeId: string) => void
}

export function GraphView({ data, width = 800, height = 600, onNodeSelect }: Props) {
  const svgRef = useRef<SVGSVGElement | null>(null)

  useEffect(() => {
    if (!svgRef.current) return

    const svg = d3.select(svgRef.current)
    svg.selectAll('*').remove()

    const simulation = d3
      .forceSimulation<GraphNode>(data.nodes)
      .force(
        'link',
        d3
          .forceLink<GraphNode, GraphEdge>(data.edges)
          .id((d) => d.id)
          .distance(90)
          .strength(0.7),
      )
      .force('charge', d3.forceManyBody().strength(-220))
      .force('center', d3.forceCenter(width / 2, height / 2))

    const link = svg
      .append('g')
      .attr('stroke', '#94a3b8')
      .attr('stroke-opacity', 0.65)
      .selectAll('line')
      .data(data.edges)
      .join('line')
      .attr('stroke-width', (d) => (d.score ? d.score * 2 : 1))

    const node = svg
      .append('g')
      .selectAll('circle')
      .data(data.nodes)
      .join('circle')
      .attr('r', (d) => (d.type === 'note' ? 12 : 7))
      .attr('fill', (d) => (d.type === 'note' ? '#3b82f6' : '#10b981'))
      .style('cursor', 'pointer')
      .on('click', (_, nodeData) => onNodeSelect?.(nodeData.id))
      .call(
        d3
          .drag<SVGCircleElement, GraphNode>()
          .on('start', dragStarted)
          .on('drag', dragged)
          .on('end', dragEnded),
      )

    const label = svg
      .append('g')
      .selectAll('text')
      .data(data.nodes)
      .join('text')
      .text((d) => d.label)
      .attr('font-size', 10)
      .attr('fill', '#0f172a')
      .attr('dx', 14)
      .attr('dy', 4)

    simulation.on('tick', () => {
      link
        .attr('x1', (d) => (d.source as GraphNode).x!)
        .attr('y1', (d) => (d.source as GraphNode).y!)
        .attr('x2', (d) => (d.target as GraphNode).x!)
        .attr('y2', (d) => (d.target as GraphNode).y!)

      node
        .attr('cx', (d) => d.x!)
        .attr('cy', (d) => d.y!)

      label
        .attr('x', (d) => d.x!)
        .attr('y', (d) => d.y!)
    })

    function dragStarted(event: d3.D3DragEvent<SVGCircleElement, GraphNode, GraphNode>, d: GraphNode) {
      if (!event.active) simulation.alphaTarget(0.3).restart()
      d.fx = d.x
      d.fy = d.y
    }

    function dragged(event: d3.D3DragEvent<SVGCircleElement, GraphNode, GraphNode>, d: GraphNode) {
      d.fx = event.x
      d.fy = event.y
    }

    function dragEnded(event: d3.D3DragEvent<SVGCircleElement, GraphNode, GraphNode>, d: GraphNode) {
      if (!event.active) simulation.alphaTarget(0)
      d.fx = null
      d.fy = null
    }

    return () => {
      simulation.stop()
    }
  }, [data, height, onNodeSelect, width])

  return <svg ref={svgRef} width={width} height={height} className="h-full w-full" />
}
